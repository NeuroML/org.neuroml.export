

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
	port(
		clk		: In  Std_logic;
		rst		: In  Std_logic;
		Start	: In  Std_logic;
		Done	: Out  Std_logic;
		A		: In sfixed(20 downto -20);
		X		: In sfixed(20 downto -20);
		Output	: Out sfixed(20 downto -20)
		);
end ParamPow; 

architecture RTL of ParamPow is
signal output_internal : sfixed(20 downto -20);
signal count : sfixed(20 downto -20);
begin
	 
	process(clk)
		variable Sel : integer;
		begin
			if rst = '1' then
				count <= to_sfixed(1,20, -20);
				output_internal <= to_sfixed (0,20, -20);
			elsif clk'event and clk = '1' then
				if Start = '1' then
					count <= to_sfixed(1,20, -20);
					output_internal <= A;
						Done <= '0';
				else
					if To_slv ( resize (count - X   ,20, -20))(40) = '1' then
						count <= resize (count + to_sfixed(1,1,0)   ,20, -20);
						output_internal <= resize (output_internal * A,20, -20);
					else
						Done <= '1';
					end if;
				end if;
			end if;
	end process;
	Output <= output_internal;
end RTL;
		