

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

entity ParamPow is
	generic( 
		BIT_TOP 	: integer := 20;	
		BIT_BOTTOM	: integer := -20);	
	port(
		clk		: In  Std_logic;
		init_model : in STD_LOGIC; --signal to all components to go into their init state
		Start	: In  Std_logic;
		Done	: Out  Std_logic;
		A		: In sfixed(BIT_TOP downto BIT_BOTTOM);
		X		: In sfixed(BIT_TOP downto BIT_BOTTOM);
		Output	: Out sfixed(BIT_TOP downto BIT_BOTTOM)
		);
end ParamPow; 

architecture RTL of ParamPow is
signal output_internal : sfixed(BIT_TOP downto BIT_BOTTOM);
signal output_internal_next : sfixed(BIT_TOP downto BIT_BOTTOM);
signal count : sfixed(BIT_TOP downto BIT_BOTTOM);
signal count_next : sfixed(BIT_TOP downto BIT_BOTTOM);
signal done_next : std_logic;
begin
	 
	process(A,start,init_model,count,X,output_internal,init_model)
	variable Sel : integer;
	begin
		output_internal_next <= output_internal;
		count_next <= count;
		done_next <= '0';
		if init_model = '1' then
			output_internal_next <= to_sfixed(0,BIT_TOP, BIT_BOTTOM);
			count_next <= to_sfixed(1,BIT_TOP, BIT_BOTTOM);
			done_next <= '1';
		else
			if start = '1' then
				output_internal_next <= A;
				count_next <= to_sfixed(1,BIT_TOP, BIT_BOTTOM);
				done_next <= '0';
			else
				if To_slv ( resize (count - X   ,BIT_TOP, BIT_BOTTOM))(BIT_TOP-BIT_BOTTOM) = '1' then
					count_next <= resize (count + to_sfixed(1,1,0)   ,BIT_TOP, BIT_BOTTOM);
					output_internal_next <= resize (output_internal * A,BIT_TOP, BIT_BOTTOM);
					done_next <= '0';
				else
					output_internal_next <= output_internal;
					count_next <= count;
					done_next <= '1';
				end if;
			end if;
		end if;
	end process;

	process(clk)
	variable Sel : integer;
	begin
		if clk'event and clk = '1' then
			output_internal <= output_internal_next;
			count <= count_next;
			Done <= done_next;
		end if;
	end process;
	Output <= output_internal;
end RTL;
		

