--
-- Parameterisable N to M mux.
--

library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

-- For Modelsim
--use ieee.fixed_pkg.all;
--use ieee.fixed_float_types.ALL;

-- For ISE
library ieee_proposed;
use ieee_proposed.fixed_pkg.all;
use ieee_proposed.fixed_float_types.ALL;
use IEEE.numeric_std.all;

entity ParamExp is
	generic( 
		BIT_TOP 	: integer := 20;	
		BIT_BOTTOM	: integer := -20);	
	port(
		clk		: In  Std_logic;
		rst		: In  Std_logic;
		Start	: In  Std_logic;
		Done	: Out  Std_logic;
		X		: In sfixed(BIT_TOP downto BIT_BOTTOM);
		Output	: Out sfixed(BIT_TOP downto BIT_BOTTOM)
		);
end ParamExp;

architecture RTL of ParamExp is
type MEM is array (0 to 7) of sfixed(BIT_TOP downto BIT_BOTTOM);
signal output_internal : sfixed(BIT_TOP downto BIT_BOTTOM);
signal output_integer : sfixed(BIT_TOP downto BIT_BOTTOM);
signal currentTerm : sfixed(BIT_TOP downto BIT_BOTTOM);
signal COUNT : unsigned(3 downto 0) := "1001";
signal COUNT_INT : sfixed(BIT_TOP downto 0) := to_sfixed (0,BIT_TOP, 0);
signal DONEFRACTION : STD_LOGIC := '0';
signal DONEINTEGER : STD_LOGIC := '0';
signal ISPOSITIVE : STD_LOGIC := '0';
signal ISGREATERTHANONE : STD_LOGIC := '0';
signal X_integer : sfixed(BIT_TOP downto 0);
signal X_fraction : sfixed(0 downto BIT_BOTTOM);
begin

	process(X,clk)
	begin
		X_integer <= resize(abs(X) - 0.5,BIT_TOP,0);
		X_fraction <= resize(abs(X) - X_integer,0,BIT_BOTTOM);
		if X > 0 then
			ISPOSITIVE <= '1';
		else
			ISPOSITIVE <= '0';
		end if;
	end process;

	process(clk)
		variable MEM8Xsfixed : MEM := (to_sfixed (1,BIT_TOP, BIT_BOTTOM),to_sfixed (0.5,BIT_TOP, BIT_BOTTOM),to_sfixed (0.33333333,BIT_TOP, BIT_BOTTOM),to_sfixed (0.25,BIT_TOP, BIT_BOTTOM),
		to_sfixed (0.2,BIT_TOP, BIT_BOTTOM),to_sfixed (0.16666666667,BIT_TOP, BIT_BOTTOM),to_sfixed (0.142857142857,BIT_TOP, BIT_BOTTOM),to_sfixed (0.125,BIT_TOP, BIT_BOTTOM));
		variable Sel : integer;
		begin 
			if rst = '1' then
				DONEFRACTION <= '0';
				COUNT <= "1001";
				currentTerm <= to_sfixed (0,BIT_TOP, BIT_BOTTOM);
				output_internal <= to_sfixed (0,BIT_TOP, BIT_BOTTOM);
			elsif clk'event and clk = '1' then
				if Start = '1' then
					DONEFRACTION <= '0';
					COUNT <= "0000"; 	
					currentTerm <= to_sfixed (1,BIT_TOP, BIT_BOTTOM);
					output_internal <= to_sfixed (0,BIT_TOP, BIT_BOTTOM);
				elsif COUNT /= "1001" then
					DONEFRACTION <= '0';
					if (ISPOSITIVE = '1') then
						output_internal <= resize(output_internal + currentTerm,BIT_TOP, BIT_BOTTOM);
					else
						if (COUNT(0) = '1') then
							output_internal <= resize(output_internal - currentTerm,BIT_TOP, BIT_BOTTOM);
						else
							output_internal <= resize(output_internal + currentTerm,BIT_TOP, BIT_BOTTOM);
						end if;
					end if;
					currentTerm <= resize(MEM8Xsfixed(to_integer(unsigned(COUNT(2 downto 0)))) * resize(X_fraction * currentTerm,BIT_TOP, BIT_BOTTOM),BIT_TOP, BIT_BOTTOM);
					COUNT <= COUNT + 1;
				elsif COUNT = "1001" then  
					DONEFRACTION <= '1';
				end if; 
			end if;
	end process;


	
	process(clk)
		variable E : sfixed(BIT_TOP downto BIT_BOTTOM) := to_sfixed(2.71828182845904523536028747135266249775724709369995,BIT_TOP,BIT_BOTTOM);
		variable EInv : sfixed(BIT_TOP downto BIT_BOTTOM) := resize(reciprocal(to_sfixed(2.71828182845904523536028747135266249775724709369995,BIT_TOP,BIT_BOTTOM)),BIT_TOP,BIT_BOTTOM);
		begin
		if rst = '1' then
			DONEINTEGER <= '0';
			COUNT_INT <= X_integer;
			output_integer <= to_sfixed (1,BIT_TOP, BIT_BOTTOM);
		elsif clk'event and clk = '1' then
			if Start = '1' then
				DONEINTEGER <= '0';
				COUNT_INT <= X_integer; 	
				output_integer <= to_sfixed (1,BIT_TOP, BIT_BOTTOM);
			elsif COUNT_INT > 0 then
				DONEINTEGER <= '0'; 
				if (ISPOSITIVE = '1') then
					output_integer <=resize(output_integer * E,BIT_TOP,BIT_BOTTOM);
				else
					output_integer <=resize(output_integer * EInv,BIT_TOP,BIT_BOTTOM);
				end if;
				COUNT_INT <= resize(COUNT_INT - 1,BIT_TOP,0);
			elsif COUNT_INT = 0 then 
				DONEINTEGER <= '1';
			end if; 
		end if;
	end process;

	Output <=  resize(output_internal * output_integer,BIT_TOP, BIT_BOTTOM);
	Done <= DONEFRACTION AND DONEINTEGER;
	
	
	--process (DONEFRACTION) 
	--begin
	--	if (DONEFRACTION'event or DONEINTEGER'event) and DONEFRACTION = '1' and DONEINTEGER = '1' then
	--		report "The value of X_integer = " & real'image(to_real(X_integer)) & " and X_fraction " & real'image(to_real(X_fraction));
	--		report "The value of exp( " & real'image(to_real(X)) & " ) = " & 
	--			real'image(to_real(output_internal)) & " * " & real'image(to_real(output_integer));
	--	end if;
	--end process;
		
end RTL; 
		


